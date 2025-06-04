import React, { useState, useEffect } from 'react'
import { Table, Button, SearchBar, Modal, Form, Input, Card, Space } from '@/components'
import voiceprintApi from '@/apis/module/voiceprintAgent'

// 声纹设备管理界面
const VoiceprintManagement = () => {
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [empty, setEmpty] = useState(false)
  const [searchParams, setSearchParams] = useState({})
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [visible, setVisible] = useState(false)
  const [form] = Form.useForm()

  const fetchData = async () => {
    setLoading(true)
    try {
      const res = await voiceprintApi.list({ ...searchParams, page, pageSize })
      if (Array.isArray(res)) {
        setList(res)
        setEmpty(res.length === 0)
      } else {
        setEmpty(true)
        setError('数据格式异常')
      }
    } catch (err) {
      setError('加载声纹列表失败')
      console.error('获取数据失败:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleDelete = async (id) => {
    try {
      await voiceprintApi.delete(id)
      fetchData()
    } catch (err) {
      setError('删除失败')
      console.error('删除失败:', err)
    }
  }

  const handleExport = async () => {
    try {
      const res = await voiceprintApi.export()
      const url = URL.createObjectURL(new Blob([res]))
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', 'voiceprints.csv')
      document.body.appendChild(link)
      link.click()
      link.remove()
    } catch (err) {
      setError('导出失败')
      console.error('导出失败:', err)
    }
  }

  const handleAdd = async () => {
    try {
      const values = await form.validateFields()
      await voiceprintApi.create(values)
      setVisible(false)
      fetchData()
    } catch (err) {
      console.error('新增失败:', err)
    }
  }

  useEffect(() => {
    fetchData()
  }, [page, pageSize, searchParams])

  return (
    <Card
      title="声纹设备管理"
      extra={
        <Space>
          <Button type="primary" onClick={() => setVisible(true)}>
            新增声纹
          </Button>
          <Button onClick={handleExport}>导出</Button>
          <SearchBar
            placeholder="请输入声纹名称"
            onSearch={(value) => setSearchParams({ name: value })}
            style={{ width: 200 }}
          />
        </Space>
      }
    >
      <Table
        dataSource={list}
        loading={loading}
        pagination={{
          current: page,
          pageSize,
          total: list.length, // 可替换为后端返回的总数
          onChange: (current, size) => {
            setPage(current)
            setPageSize(size)
          },
        }}
        columns={[
          { title: '声纹ID', dataIndex: 'id' },
          { title: '声纹名称', dataIndex: 'name' },
          { title: '创建时间', dataIndex: 'createTime' },
          { title: '操作', render: (_, record) => (
            <Button type="link" onClick={() => handleDelete(record.id)}>删除</Button>
          )},
        ]}
      />

      <Modal
        title="新增声纹"
        visible={visible}
        onOk={handleAdd}
        onCancel={() => setVisible(false)}
      >
        <Form form={form}>
          <Form.Item label="声纹名称" name="name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}

export default VoiceprintManagement